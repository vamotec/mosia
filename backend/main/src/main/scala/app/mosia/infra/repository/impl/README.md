## 模型逻辑

### Users

    Dto > Controller > Service > 分解为字段后+辅助领域模型 > Repo > 主领域模型 > Dao
    接口层 ====================== 业务层 ================================= 持久层

    Email在Dto就转格式，password传递到领域模型后再转Password